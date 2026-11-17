<template>
  <div class="sidebar-logo-container" :class="{'collapse':collapse}">
    <div class="logo-glow"></div>
    <div class="logo-grid"></div>
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <svg viewBox="0 0 100 100" class="sidebar-logo animated-logo">
          <defs>
            <linearGradient id="logoIconGradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
              <stop offset="100%" style="stop-color:#0099ff;stop-opacity:1" />
            </linearGradient>
            <radialGradient id="rippleGradient1" cx="50%" cy="50%" r="50%">
              <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:0.8" />
              <stop offset="100%" style="stop-color:#00d4ff;stop-opacity:0" />
            </radialGradient>
            <radialGradient id="rippleGradient2" cx="50%" cy="50%" r="50%">
              <stop offset="0%" style="stop-color:#00ff88;stop-opacity:0.6" />
              <stop offset="100%" style="stop-color:#00ff88;stop-opacity:0" />
            </radialGradient>
          </defs>
          <circle cx="50" cy="50" r="45" fill="none" stroke="url(#logoIconGradient)" stroke-width="2" />
          <circle cx="50" cy="50" r="35" fill="none" stroke="url(#logoIconGradient)" stroke-width="1" opacity="0.6" />
          <circle cx="50" cy="50" r="25" fill="none" stroke="url(#logoIconGradient)" stroke-width="1" opacity="0.3" />
          <line x1="50" y1="5" x2="50" y2="15" stroke="url(#logoIconGradient)" stroke-width="2" />
          <line x1="50" y1="85" x2="50" y2="95" stroke="url(#logoIconGradient)" stroke-width="2" />
          <line x1="5" y1="50" x2="15" y2="50" stroke="url(#logoIconGradient)" stroke-width="2" />
          <line x1="85" y1="50" x2="95" y2="50" stroke="url(#logoIconGradient)" stroke-width="2" />
          <rect x="45" y="45" width="10" height="10" fill="url(#logoIconGradient)" class="center-node" />
          <circle cx="50" cy="50" r="50" fill="url(#rippleGradient1)" class="ripple ripple-1" />
          <circle cx="50" cy="50" r="50" fill="url(#rippleGradient2)" class="ripple ripple-2" />
        </svg>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <svg viewBox="0 0 100 100" class="sidebar-logo animated-logo">
          <defs>
            <linearGradient id="logoIconGradientExp" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:1" />
              <stop offset="100%" style="stop-color:#0099ff;stop-opacity:1" />
            </linearGradient>
            <radialGradient id="rippleGradientExp1" cx="50%" cy="50%" r="50%">
              <stop offset="0%" style="stop-color:#00d4ff;stop-opacity:0.8" />
              <stop offset="100%" style="stop-color:#00d4ff;stop-opacity:0" />
            </radialGradient>
            <radialGradient id="rippleGradientExp2" cx="50%" cy="50%" r="50%">
              <stop offset="0%" style="stop-color:#00ff88;stop-opacity:0.6" />
              <stop offset="100%" style="stop-color:#00ff88;stop-opacity:0" />
            </radialGradient>
          </defs>
          <circle cx="50" cy="50" r="45" fill="none" stroke="url(#logoIconGradientExp)" stroke-width="2" />
          <circle cx="50" cy="50" r="35" fill="none" stroke="url(#logoIconGradientExp)" stroke-width="1" opacity="0.6" />
          <circle cx="50" cy="50" r="25" fill="none" stroke="url(#logoIconGradientExp)" stroke-width="1" opacity="0.3" />
          <line x1="50" y1="5" x2="50" y2="15" stroke="url(#logoIconGradientExp)" stroke-width="2" />
          <line x1="50" y1="85" x2="50" y2="95" stroke="url(#logoIconGradientExp)" stroke-width="2" />
          <line x1="5" y1="50" x2="15" y2="50" stroke="url(#logoIconGradientExp)" stroke-width="2" />
          <line x1="85" y1="50" x2="95" y2="50" stroke="url(#logoIconGradientExp)" stroke-width="2" />
          <rect x="45" y="45" width="10" height="10" fill="url(#logoIconGradientExp)" class="center-node" />
          <circle cx="50" cy="50" r="50" fill="url(#rippleGradientExp1)" class="ripple ripple-1" />
          <circle cx="50" cy="50" r="50" fill="url(#rippleGradientExp2)" class="ripple ripple-2" />
        </svg>
        <h1 class="sidebar-title">{{ title }}</h1>
      </router-link>
    </transition>
  </div>
</template>

<script>
import variables from '@/assets/styles/variables.scss'

export default {
  name: 'SidebarLogo',
  props: {
    collapse: {
      type: Boolean,
      required: true
    }
  },
  computed: {
    variables() {
      return variables
    },
    sideTheme() {
      return this.$store.state.settings.sideTheme
    },
    navType() {
      return this.$store.state.settings.navType
    }
  },
  data() {
    return {
      title: process.env.VUE_APP_TITLE
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 0.3s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

@keyframes logoPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.85;
  }
}

@keyframes rippleExpand {
  0% {
    transform: scale(0.3);
    opacity: 0.8;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

@keyframes nodeGlow {
  0%, 100% {
    filter: drop-shadow(0 0 4px rgba(0, 212, 255, 0.8));
  }
  50% {
    filter: drop-shadow(0 0 12px rgba(0, 212, 255, 1));
  }
}

@keyframes titleGlow {
  0%, 100% {
    filter: drop-shadow(0 0 10px rgba(0, 212, 255, 0.5));
    opacity: 1;
  }
  50% {
    filter: drop-shadow(0 0 20px rgba(0, 212, 255, 0.8));
    opacity: 0.95;
  }
}

@keyframes gridMove {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 20px 20px;
  }
}

@keyframes glowMove {
  0%, 100% {
    transform: translate(0, 0);
    opacity: 0.08;
  }
  50% {
    transform: translate(10px, 10px);
    opacity: 0.12;
  }
}

.sidebar-logo-container {
  position: relative;
  height: 50px;
  line-height: 50px;
  background: linear-gradient(180deg, #0a0a1a 0%, #0d1b2a 100%);
  text-align: center;
  overflow: hidden;
  border-bottom: 1px solid rgba(0, 212, 255, 0.1);

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.3), transparent);
    animation: titleGlow 3s ease-in-out infinite;
  }

  & .logo-glow {
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(ellipse at center top, rgba(0, 212, 255, 0.08) 0%, transparent 60%);
    pointer-events: none;
    animation: glowMove 8s ease-in-out infinite;
  }

  & .logo-grid {
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(0, 212, 255, 0.03) 1px, transparent 1px),
      linear-gradient(90deg, rgba(0, 212, 255, 0.03) 1px, transparent 1px);
    background-size: 20px 20px;
    pointer-events: none;
    animation: gridMove 20s linear infinite;
  }

  & .sidebar-logo-link {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    width: 100%;
    text-decoration: none;

    & .sidebar-logo {
      width: 30px;
      height: 30px;
      vertical-align: middle;
      margin-right: 10px;
      filter: drop-shadow(0 0 6px rgba(0, 212, 255, 0.5));
      animation: logoPulse 3s ease-in-out infinite;

      & .center-node {
        animation: nodeGlow 2s ease-in-out infinite;
      }

      & .ripple {
        opacity: 0;

        &.ripple-1 {
          animation: rippleExpand 2.5s ease-out infinite;
        }

        &.ripple-2 {
          animation: rippleExpand 2.5s ease-out infinite;
          animation-delay: 1.25s;
        }
      }
    }

    & .sidebar-title {
      display: inline-block;
      margin: 0;
      font-weight: 700;
      line-height: 50px;
      font-size: 15px;
      font-family: 'Microsoft YaHei', Avenir, Helvetica Neue, Arial, sans-serif;
      vertical-align: middle;
      background: linear-gradient(135deg, #00d4ff 0%, #00ff88 50%, #00d4ff 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      letter-spacing: 1px;
      animation: titleGlow 3s ease-in-out infinite;
    }
  }

  &.collapse {
    .sidebar-logo {
      margin-right: 0px;
      width: 28px;
      height: 28px;
    }
  }
}
</style>
