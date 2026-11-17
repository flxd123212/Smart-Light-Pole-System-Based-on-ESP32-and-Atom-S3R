<template>
  <div class="register-container">
    <div class="particle-bg">
      <canvas ref="particleCanvas"></canvas>
    </div>
    <div class="bg-gradient"></div>
    <div class="grid-pattern"></div>
    <div class="light-rays"></div>

    <div class="register-wrapper">
      <div class="brand-section">
        <div class="brand-icon">
          <svg viewBox="0 0 100 100" class="icon">
            <defs>
              <linearGradient id="iconGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
                <stop offset="100%" style="stop-color:#0099ff;stop-opacity:1" />
              </linearGradient>
            </defs>
            <circle cx="50" cy="50" r="45" fill="none" stroke="url(#iconGradient)" stroke-width="2" />
            <circle cx="50" cy="50" r="35" fill="none" stroke="url(#iconGradient)" stroke-width="1" opacity="0.6" />
            <circle cx="50" cy="50" r="25" fill="none" stroke="url(#iconGradient)" stroke-width="1" opacity="0.3" />
            <line x1="50" y1="5" x2="50" y2="15" stroke="url(#iconGradient)" stroke-width="2" />
            <line x1="50" y1="85" x2="50" y2="95" stroke="url(#iconGradient)" stroke-width="2" />
            <line x1="5" y1="50" x2="15" y2="50" stroke="url(#iconGradient)" stroke-width="2" />
            <line x1="85" y1="50" x2="95" y2="50" stroke="url(#iconGradient)" stroke-width="2" />
            <line x1="18" y1="18" x2="25" y2="25" stroke="url(#iconGradient)" stroke-width="1.5" />
            <line x1="75" y1="75" x2="82" y2="82" stroke="url(#iconGradient)" stroke-width="1.5" />
            <line x1="82" y1="18" x2="75" y2="25" stroke="url(#iconGradient)" stroke-width="1.5" />
            <line x1="25" y1="75" x2="18" y2="82" stroke="url(#iconGradient)" stroke-width="1.5" />
            <rect x="45" y="45" width="10" height="10" fill="url(#iconGradient)" />
          </svg>
        </div>
        <h1 class="brand-title">IoT Light</h1>
        <p class="brand-subtitle">智能灯杆管理平台</p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-icon">📡</span>
            <span>实时监控</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">⚡</span>
            <span>智能控制</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🔒</span>
            <span>安全防护</span>
          </div>
        </div>
        <div class="brand-stats">
          <div class="stat-item">
            <span class="stat-value">100+</span>
            <span class="stat-label">灯杆设备</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">24/7</span>
            <span class="stat-label">全天候</span>
          </div>
        </div>
      </div>

      <div class="register-card">
        <div class="card-header">
          <h2 class="card-title">创建账户</h2>
          <p class="card-desc">开启智能灯杆管理之旅</p>
        </div>

        <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="register-form">
          <el-form-item prop="username">
            <div class="input-wrapper">
              <svg-icon slot="prefix" icon-class="user" class="input-icon" />
              <el-input
                v-model="registerForm.username"
                type="text"
                auto-complete="off"
                placeholder="请输入账号"
                class="dark-input"
              />
            </div>
          </el-form-item>

          <el-form-item prop="password" :rules="registerPwdValidator">
            <div class="input-wrapper">
              <svg-icon slot="prefix" icon-class="password" class="input-icon" />
              <el-input
                v-model="registerForm.password"
                type="password"
                auto-complete="off"
                placeholder="请输入密码"
                @keyup.enter.native="handleRegister"
                class="dark-input"
              />
            </div>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <div class="input-wrapper">
              <svg-icon slot="prefix" icon-class="password" class="input-icon" />
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                auto-complete="off"
                placeholder="请确认密码"
                @keyup.enter.native="handleRegister"
                class="dark-input"
              />
            </div>
          </el-form-item>

          <el-form-item prop="code" v-if="captchaEnabled">
            <div class="code-wrapper">
              <div class="input-wrapper code-input">
                <svg-icon slot="prefix" icon-class="validCode" class="input-icon" />
                <el-input
                  v-model="registerForm.code"
                  auto-complete="off"
                  placeholder="请输入验证码"
                  @keyup.enter.native="handleRegister"
                  class="dark-input"
                />
              </div>
              <div class="code-img" @click="getCode">
                <img :src="codeUrl" class="captcha-img" />
              </div>
            </div>
          </el-form-item>

          <div class="form-options">
            <span class="login-link">
              <router-link to="/login">返回登录</router-link>
            </span>
          </div>

          <el-form-item class="register-btn-item">
            <el-button
              :loading="loading"
              size="medium"
              type="primary"
              class="register-btn"
              @click.native.prevent="handleRegister"
            >
              <span v-if="!loading">注 册</span>
              <span v-else>注 册 中...</span>
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="register-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script>
import { getCodeImg, register } from "@/api/login"
import passwordRule from "@/utils/passwordRule"
import defaultSettings from '@/settings'

export default {
  mixins: [passwordRule],
  name: "Register",
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      footerContent: defaultSettings.footerContent,
      codeUrl: "",
      registerForm: {
        username: "",
        password: "",
        confirmPassword: "",
        code: "",
        uuid: ""
      },
      loading: false,
      captchaEnabled: true,
      particles: [],
      animationId: null
    }
  },
  computed: {
    registerRules() {
      return {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" },
          { min: 2, max: 20, message: '用户账号长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: "请再次输入您的密码", trigger: "blur" },
          {
            validator: (rule, value, callback) => {
              if (this.registerForm.password !== value) {
                callback(new Error("两次输入的密码不一致"))
              } else {
                callback()
              }
            }, trigger: "blur"
          }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      }
    }
  },
  mounted() {
    this.initParticles()
    this.animateParticles()
  },
  beforeDestroy() {
    if (this.animationId) {
      cancelAnimationFrame(this.animationId)
    }
  },
  created() {
    this.getCode()
  },
  methods: {
    initParticles() {
      const canvas = this.$refs.particleCanvas
      if (!canvas) return
      canvas.width = window.innerWidth
      canvas.height = window.innerHeight
      const ctx = canvas.getContext('2d')
      this.ctx = ctx

      for (let i = 0; i < 100; i++) {
        this.particles.push({
          x: Math.random() * canvas.width,
          y: Math.random() * canvas.height,
          radius: Math.random() * 2 + 0.5,
          vx: (Math.random() - 0.5) * 0.5,
          vy: (Math.random() - 0.5) * 0.5,
          opacity: Math.random() * 0.5 + 0.2,
          color: Math.random() > 0.5 ? '#00d4ff' : '#00ff88'
        })
      }
    },
    animateParticles() {
      if (!this.ctx || !this.$refs.particleCanvas) return
      const canvas = this.$refs.particleCanvas
      this.ctx.clearRect(0, 0, canvas.width, canvas.height)

      this.particles.forEach((p, index) => {
        p.x += p.vx
        p.y += p.vy

        if (p.x < 0 || p.x > canvas.width) p.vx *= -1
        if (p.y < 0 || p.y > canvas.height) p.vy *= -1

        this.ctx.beginPath()
        this.ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
        this.ctx.fillStyle = p.color
        this.ctx.globalAlpha = p.opacity
        this.ctx.fill()

        this.particles.slice(index + 1).forEach(p2 => {
          const dx = p.x - p2.x
          const dy = p.y - p2.y
          const dist = Math.sqrt(dx * dx + dy * dy)

          if (dist < 150) {
            this.ctx.beginPath()
            this.ctx.moveTo(p.x, p.y)
            this.ctx.lineTo(p2.x, p2.y)
            this.ctx.strokeStyle = p.color
            this.ctx.globalAlpha = (150 - dist) / 150 * 0.2
            this.ctx.stroke()
          }
        })
      })

      this.ctx.globalAlpha = 1
      this.animationId = requestAnimationFrame(() => this.animateParticles())
    },
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img
          this.registerForm.uuid = res.uuid
        }
      })
    },
    handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (valid) {
          this.loading = true
          register(this.registerForm).then(() => {
            const username = this.registerForm.username
            this.$alert("<font color='#00d4ff'>恭喜你，您的账号 " + username + " 注册成功！</font>", '系统提示', {
              dangerouslyUseHTMLString: true,
              type: 'success'
            }).then(() => {
              this.$router.push("/login")
            }).catch(() => {})
          }).catch(() => {
            this.loading = false
            if (this.captchaEnabled) {
              this.getCode()
            }
          })
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.register-container {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}

.particle-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #0a0a1a 0%, #0d1b2a 50%, #1b263b 100%);
  z-index: 1;
}

.grid-pattern {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image:
    linear-gradient(rgba(0, 212, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 212, 255, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  z-index: 2;
}

.light-rays {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at 30% 20%, rgba(0, 212, 255, 0.08) 0%, transparent 50%),
              radial-gradient(ellipse at 70% 80%, rgba(0, 255, 136, 0.05) 0%, transparent 50%);
  animation: rayMove 20s ease-in-out infinite;
  z-index: 3;
}

@keyframes rayMove {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(30px, 30px) rotate(5deg); }
}

.register-wrapper {
  position: relative;
  z-index: 10;
  display: flex;
  width: 900px;
  height: 550px;
  gap: 40px;
}

.brand-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  padding: 40px;
}

.brand-icon {
  width: 120px;
  height: 120px;
  margin-bottom: 20px;
  animation: iconPulse 3s ease-in-out infinite;
}

@keyframes iconPulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.05); opacity: 0.8; }
}

.icon {
  width: 100%;
  height: 100%;
}

.brand-title {
  font-size: 48px;
  font-weight: 700;
  background: linear-gradient(135deg, #00d4ff 0%, #00ff88 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 10px 0;
  letter-spacing: 2px;
}

.brand-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 40px 0;
}

.brand-features {
  display: flex;
  gap: 30px;
  margin-bottom: 40px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.feature-icon {
  font-size: 24px;
}

.brand-stats {
  display: flex;
  gap: 40px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #00d4ff;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.register-card {
  flex: 1;
  background: rgba(20, 30, 50, 0.6);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 40px;
  border: 1px solid rgba(0, 212, 255, 0.2);
  box-shadow:
    0 0 40px rgba(0, 212, 255, 0.1),
    inset 0 0 60px rgba(0, 212, 255, 0.02);
}

.card-header {
  text-align: center;
  margin-bottom: 30px;
}

.card-title {
  font-size: 28px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 8px 0;
}

.card-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 0px;
}

.input-wrapper {
  position: relative;
  width: 100%;
}

.input-icon {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(0, 212, 255, 0.6);
  font-size: 16px;
  z-index: 10;
}

.dark-input {
  width: 100%;
  height: 48px;
  padding: 0 15px 0 45px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 10px;
  color: #ffffff;
  font-size: 14px;
  transition: all 0.3s ease;

  &:focus {
    border-color: #00d4ff;
    box-shadow: 0 0 20px rgba(0, 212, 255, 0.2);
    background: rgba(0, 0, 0, 0.4);
  }

  &::placeholder {
    color: rgba(255, 255, 255, 0.5);
  }
}

::v-deep .dark-input .el-input__inner {
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 14px;
  height: 100%;
  line-height: 48px;

  &::placeholder {
    color: rgba(255, 255, 255, 0.5);
  }
}

::v-deep .dark-input .el-input__inner:focus {
  background: rgba(0, 0, 0, 0.4);
  border: none;
  box-shadow: none;
}

.code-wrapper {
  display: flex;
  gap: 10px;
}

.code-input {
  flex: 1;
}

.code-img {
  width: 100px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid rgba(0, 212, 255, 0.2);
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: #00d4ff;
    transform: scale(1.02);
  }
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-options {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 5px;
}

.login-link {
  font-size: 13px;

  a {
    color: #00d4ff;
    text-decoration: none;
    transition: color 0.3s ease;

    &:hover {
      color: #00ff88;
    }
  }
}

.register-btn-item {
  margin-top: 10px;
}

.register-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  background: linear-gradient(135deg, #00ff88 0%, #00d4ff 100%);
  border: none;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(0, 255, 136, 0.3);

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 30px rgba(0, 255, 136, 0.5);
    background: linear-gradient(135deg, #00d4ff 0%, #0099ff 100%);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }
}

.register-footer {
  position: fixed;
  bottom: 20px;
  left: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
  z-index: 10;
}

@media (max-width: 900px) {
  .register-wrapper {
    flex-direction: column;
    width: 90%;
    height: auto;
    padding: 20px;
  }

  .brand-section {
    padding: 20px;
  }

  .brand-title {
    font-size: 32px;
  }

  .brand-features {
    gap: 20px;
  }

  .register-card {
    padding: 30px;
  }
}
</style>
