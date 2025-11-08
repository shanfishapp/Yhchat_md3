package com.yhchat.canary.utils

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * RSA 加密工具类
 * 用于加密 WebDAV 挂载点请求的密钥和 IV
 * 支持 minSDK 21
 */
object RSAEncryptionUtil {
    
    private const val PUBLIC_KEY_URL = "https://chat.yhchat.com/assets/key/apps_public.pem"
    
    /**
     * 生成随机字节数组
     */
    fun generateRandomBytes(length: Int): ByteArray {
        return SecureRandom().generateSeed(length)
    }
    
    /**
     * 从 URL 获取公钥（在IO线程执行）
     */
    suspend fun getPublicKey(): ByteArray? = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("RSAEncryptionUtil", "开始获取公钥: $PUBLIC_KEY_URL")
            
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(PUBLIC_KEY_URL)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                android.util.Log.d("RSAEncryptionUtil", "公钥获取成功，大小: ${bytes?.size} 字节")
                bytes
            } else {
                android.util.Log.e("RSAEncryptionUtil", "获取公钥失败: HTTP ${response.code} ${response.message}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("RSAEncryptionUtil", "获取公钥失败", e)
            null
        }
    }
    
    /**
     * 使用 RSA 公钥加密数据（在计算线程执行）
     * @param publicKeyBytes PEM 格式的公钥字节数组
     * @param dataToEncrypt 要加密的数据
     * @return Base64 编码的加密数据，失败返回 null
     */
    suspend fun rsaEncrypt(publicKeyBytes: ByteArray, dataToEncrypt: ByteArray): String? = withContext(Dispatchers.Default) {
        return@withContext try {
            // 转换为字符串并记录原始内容
            val originalContent = String(publicKeyBytes, Charsets.UTF_8)
            android.util.Log.d("RSAEncryptionUtil", "原始公钥内容长度: ${originalContent.length}")
            
            // 移除 PEM 格式的头部和尾部，并清理所有空白字符
            var publicKeyContent = originalContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .replace("\t", "")
            
            // 移除所有非Base64字符
            publicKeyContent = publicKeyContent.replace(Regex("[^A-Za-z0-9+/=]"), "")
            
            android.util.Log.d("RSAEncryptionUtil", "清理后的公钥内容长度: ${publicKeyContent.length}")
            android.util.Log.d("RSAEncryptionUtil", "清理后的公钥内容前100字符: ${publicKeyContent.take(100)}")
            
            if (publicKeyContent.isEmpty()) {
                android.util.Log.e("RSAEncryptionUtil", "公钥内容为空")
                return@withContext null
            }
            
            // Base64 解码获取公钥字节
            val keyBytes = try {
                Base64.decode(publicKeyContent, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                android.util.Log.e("RSAEncryptionUtil", "Base64解码失败，尝试使用NO_WRAP标志", e)
                // 尝试使用NO_WRAP标志
                Base64.decode(publicKeyContent, Base64.NO_WRAP)
            }
            
            android.util.Log.d("RSAEncryptionUtil", "解码后的公钥字节长度: ${keyBytes.size}")
            
            // 尝试解析公钥，支持多种格式
            val publicKey = parsePublicKey(keyBytes, originalContent)
                ?: return@withContext null
            
            android.util.Log.d("RSAEncryptionUtil", "公钥创建成功，算法: ${publicKey.algorithm}")
            
            // 创建加密器，使用 PKCS1Padding 填充以兼容 minSDK 21
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            
            // 加密数据
            val encryptedData = cipher.doFinal(dataToEncrypt)
            
            android.util.Log.d("RSAEncryptionUtil", "加密成功，加密数据长度: ${encryptedData.size}")
            
            // Base64 编码（NO_WRAP 兼容 API 21+）
            Base64.encodeToString(encryptedData, Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("RSAEncryptionUtil", "RSA 加密失败", e)
            null
        }
    }
    
    /**
     * 准备 WebDAV 挂载点请求的加密参数（在后台线程执行）
     * @return Pair<encryptKey, encryptIv>，失败返回 null
     */
    suspend fun prepareEncryptionParams(): Pair<String, String>? = withContext(Dispatchers.IO) {
        return@withContext try {
            // 1. 获取公钥
            val publicKeyBytes = getPublicKey() ?: return@withContext null
            
            // 2. 生成随机密钥和 IV（16 字节）
            val encryptKeyRaw = generateRandomBytes(16)
            val encryptIvRaw = generateRandomBytes(16)
            
            // 3. 使用公钥加密
            val encryptKey = rsaEncrypt(publicKeyBytes, encryptKeyRaw) ?: return@withContext null
            val encryptIv = rsaEncrypt(publicKeyBytes, encryptIvRaw) ?: return@withContext null
            
            Pair(encryptKey, encryptIv)
        } catch (e: Exception) {
            android.util.Log.e("RSAEncryptionUtil", "准备加密参数失败", e)
            null
        }
    }
    
    /**
     * 解析公钥，支持多种格式
     * @param keyBytes 公钥字节数组
     * @param originalContent 原始PEM内容，用于判断格式
     * @return 解析成功的公钥，失败返回null
     */
    private fun parsePublicKey(keyBytes: ByteArray, originalContent: String): java.security.PublicKey? {
        val keyFactory = KeyFactory.getInstance("RSA")
        
        return try {
            // 1. 尝试X.509格式 (标准格式)
            android.util.Log.d("RSAEncryptionUtil", "尝试X.509格式解析")
            val x509KeySpec = X509EncodedKeySpec(keyBytes)
            keyFactory.generatePublic(x509KeySpec)
        } catch (e: Exception) {
            android.util.Log.w("RSAEncryptionUtil", "X.509格式解析失败，尝试PKCS#1格式", e)
            
            try {
                // 2. 尝试PKCS#1格式 (RSA PUBLIC KEY)
                android.util.Log.d("RSAEncryptionUtil", "尝试PKCS#1格式解析")
                val pkcs1PublicKey = convertPkcs1ToX509(keyBytes)
                val x509KeySpec = X509EncodedKeySpec(pkcs1PublicKey)
                keyFactory.generatePublic(x509KeySpec)
            } catch (e2: Exception) {
                android.util.Log.e("RSAEncryptionUtil", "所有格式解析都失败", e2)
                
                // 3. 尝试直接使用RSAPublicKeySpec (最后的尝试)
                try {
                    android.util.Log.d("RSAEncryptionUtil", "尝试直接RSA解析")
                    parseRsaPublicKeyDirect(keyBytes)
                } catch (e3: Exception) {
                    android.util.Log.e("RSAEncryptionUtil", "直接RSA解析也失败", e3)
                    null
                }
            }
        }
    }
    
    /**
     * 将PKCS#1格式转换为X.509格式
     */
    private fun convertPkcs1ToX509(pkcs1Bytes: ByteArray): ByteArray {
        android.util.Log.d("RSAEncryptionUtil", "开始PKCS#1到X.509转换，输入长度: ${pkcs1Bytes.size}")
        
        // RSA公钥的OID序列: 1.2.840.113549.1.1.1 (RSA加密)
        val rsaOidSequence = byteArrayOf(
            0x30, 0x0d, // SEQUENCE, 长度13
            0x06, 0x09, // OID, 长度9
            0x2a, 0x86.toByte(), 0x48, 0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01, // RSA OID
            0x05, 0x00  // NULL
        )
        
        // 计算BIT STRING的长度 (PKCS#1数据 + 1个未使用位字节)
        val bitStringLength = pkcs1Bytes.size + 1
        val bitStringLengthBytes = encodeDerLength(bitStringLength)
        
        // 计算总的SEQUENCE长度
        val totalContentLength = rsaOidSequence.size + 1 + bitStringLengthBytes.size + bitStringLength
        val totalLengthBytes = encodeDerLength(totalContentLength)
        
        // 构建完整的X.509公钥
        val x509Bytes = ByteArray(1 + totalLengthBytes.size + totalContentLength)
        var offset = 0
        
        // 外层SEQUENCE标记
        x509Bytes[offset++] = 0x30
        
        // 外层SEQUENCE长度
        System.arraycopy(totalLengthBytes, 0, x509Bytes, offset, totalLengthBytes.size)
        offset += totalLengthBytes.size
        
        // RSA OID序列
        System.arraycopy(rsaOidSequence, 0, x509Bytes, offset, rsaOidSequence.size)
        offset += rsaOidSequence.size
        
        // BIT STRING标记
        x509Bytes[offset++] = 0x03
        
        // BIT STRING长度
        System.arraycopy(bitStringLengthBytes, 0, x509Bytes, offset, bitStringLengthBytes.size)
        offset += bitStringLengthBytes.size
        
        // 未使用的位数 (总是0)
        x509Bytes[offset++] = 0x00
        
        // PKCS#1公钥数据
        System.arraycopy(pkcs1Bytes, 0, x509Bytes, offset, pkcs1Bytes.size)
        
        android.util.Log.d("RSAEncryptionUtil", "X.509转换完成，输出长度: ${x509Bytes.size}")
        return x509Bytes
    }
    
    /**
     * 编码DER长度字段
     */
    private fun encodeDerLength(length: Int): ByteArray {
        return if (length < 128) {
            // 短格式
            byteArrayOf(length.toByte())
        } else if (length < 256) {
            // 长格式，1字节
            byteArrayOf(0x81.toByte(), length.toByte())
        } else if (length < 65536) {
            // 长格式，2字节
            byteArrayOf(0x82.toByte(), (length shr 8).toByte(), (length and 0xFF).toByte())
        } else {
            // 长格式，3字节 (理论上足够了)
            byteArrayOf(
                0x83.toByte(),
                (length shr 16).toByte(),
                ((length shr 8) and 0xFF).toByte(),
                (length and 0xFF).toByte()
            )
        }
    }
    
    /**
     * 直接解析RSA公钥 (最后的尝试)
     */
    private fun parseRsaPublicKeyDirect(keyBytes: ByteArray): java.security.PublicKey? {
        // 这是一个简化的实现，实际情况可能需要更复杂的ASN.1解析
        // 对于大多数情况，前面的方法应该已经足够了
        android.util.Log.w("RSAEncryptionUtil", "直接RSA解析暂未实现，返回null")
        return null
    }
}

