package tslc.beihaiyun.lyra.entity.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 邮箱验证值对象
 * 封装邮箱验证状态和相关业务逻辑
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class EmailVerification {

    /**
     * 邮箱验证状态
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean verified = false;

    /**
     * 邮箱验证时间
     */
    @Column(name = "email_verified_at")
    private LocalDateTime verifiedAt;

    /**
     * 验证码发送时间
     */
    @Column(name = "verification_code_sent_at")
    private LocalDateTime codeSentAt;

    /**
     * 验证码过期时间（分钟）
     */
    public static final Long VERIFICATION_CODE_EXPIRY_MINUTES = 30L;

    /**
     * 验证码重发间隔（分钟）
     */
    public static final Long RESEND_INTERVAL_MINUTES = 5L;

    /**
     * 创建未验证状态
     * 
     * @return 未验证的邮箱验证对象
     */
    public static EmailVerification createUnverified() {
        return new EmailVerification(false, null, null);
    }

    /**
     * 创建已验证状态
     * 
     * @return 已验证的邮箱验证对象
     */
    public static EmailVerification createVerified() {
        return new EmailVerification(true, LocalDateTime.now(), null);
    }

    /**
     * 创建待验证状态（已发送验证码）
     * 
     * @return 待验证的邮箱验证对象
     */
    public static EmailVerification createPendingVerification() {
        return new EmailVerification(false, null, LocalDateTime.now());
    }

    /**
     * 检查是否已验证
     * 
     * @return 是否已验证
     */
    public boolean isVerified() {
        return Boolean.TRUE.equals(verified);
    }

    /**
     * 检查是否需要验证
     * 
     * @return 是否需要验证
     */
    public boolean needsVerification() {
        return !isVerified();
    }

    /**
     * 验证邮箱
     * 
     * @return 新的已验证状态
     */
    public EmailVerification verify() {
        return new EmailVerification(true, LocalDateTime.now(), codeSentAt);
    }

    /**
     * 重置验证状态
     * 
     * @return 新的未验证状态
     */
    public EmailVerification resetVerification() {
        return new EmailVerification(false, null, null);
    }

    /**
     * 发送验证码
     * 
     * @return 新的待验证状态
     */
    public EmailVerification sendVerificationCode() {
        if (!canSendVerificationCode()) {
            throw new IllegalStateException("尚未到达可重发验证码的时间");
        }
        return new EmailVerification(false, verifiedAt, LocalDateTime.now());
    }

    /**
     * 检查是否可以发送验证码
     * 
     * @return 是否可以发送
     */
    public boolean canSendVerificationCode() {
        if (isVerified()) {
            return false; // 已验证的邮箱不需要再发送验证码
        }
        
        if (codeSentAt == null) {
            return true; // 从未发送过验证码
        }
        
        // 检查是否超过重发间隔
        return ChronoUnit.MINUTES.between(codeSentAt, LocalDateTime.now()) >= RESEND_INTERVAL_MINUTES;
    }

    /**
     * 检查验证码是否已过期
     * 
     * @return 是否已过期
     */
    public boolean isVerificationCodeExpired() {
        if (codeSentAt == null) {
            return true; // 没有发送过验证码
        }
        
        return ChronoUnit.MINUTES.between(codeSentAt, LocalDateTime.now()) > VERIFICATION_CODE_EXPIRY_MINUTES;
    }

    /**
     * 获取距离可重发验证码的剩余时间（分钟）
     * 
     * @return 剩余时间，如果可以立即发送则返回0
     */
    public long getMinutesUntilCanResend() {
        if (canSendVerificationCode()) {
            return 0L;
        }
        
        if (codeSentAt == null) {
            return 0L;
        }
        
        long minutesSinceSent = ChronoUnit.MINUTES.between(codeSentAt, LocalDateTime.now());
        return Math.max(0L, RESEND_INTERVAL_MINUTES - minutesSinceSent);
    }

    /**
     * 获取验证码剩余有效时间（分钟）
     * 
     * @return 剩余有效时间，如果已过期则返回0
     */
    public long getVerificationCodeRemainingMinutes() {
        if (codeSentAt == null) {
            return 0L;
        }
        
        long minutesSinceSent = ChronoUnit.MINUTES.between(codeSentAt, LocalDateTime.now());
        return Math.max(0L, VERIFICATION_CODE_EXPIRY_MINUTES - minutesSinceSent);
    }

    /**
     * 检查验证状态是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        // 如果已验证，必须有验证时间
        if (Boolean.TRUE.equals(verified)) {
            return verifiedAt != null;
        }
        
        // 如果未验证但有验证时间，则无效
        if (Boolean.FALSE.equals(verified) && verifiedAt != null) {
            return false;
        }
        
        return true;
    }

    /**
     * 获取验证状态描述
     * 
     * @return 状态描述
     */
    public String getStatusDescription() {
        if (isVerified()) {
            return "已验证";
        }
        
        if (codeSentAt == null) {
            return "未验证";
        }
        
        if (isVerificationCodeExpired()) {
            return "验证码已过期";
        }
        
        return "待验证";
    }

    /**
     * 检查是否刚刚验证（在指定分钟内）
     * 
     * @param withinMinutes 时间范围（分钟）
     * @return 是否刚刚验证
     */
    public boolean isRecentlyVerified(long withinMinutes) {
        if (!isVerified() || verifiedAt == null) {
            return false;
        }
        
        return ChronoUnit.MINUTES.between(verifiedAt, LocalDateTime.now()) <= withinMinutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailVerification that = (EmailVerification) o;
        return Objects.equals(verified, that.verified) && 
               Objects.equals(verifiedAt, that.verifiedAt) && 
               Objects.equals(codeSentAt, that.codeSentAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(verified, verifiedAt, codeSentAt);
    }

    @Override
    public String toString() {
        return "EmailVerification{" +
                "verified=" + verified +
                ", verifiedAt=" + verifiedAt +
                ", codeSentAt=" + codeSentAt +
                ", status='" + getStatusDescription() + '\'' +
                '}';
    }
} 