import request from '@/utils/request'
import type { 
  CaptchaVO,
  Result,
  UserLoginDTO, 
  UserRegisterDTO
} from '@/types/api'

// 获取登录验证码
export function getCaptcha(): Promise<Result<CaptchaVO>> {
  return request.post('/user/captcha')
}

// 用户登录
export function login(data: UserLoginDTO) {
  return request.post('/user/login', data)
}

// 用户注册
export function register(data: UserRegisterDTO) {
  return request.post('/user/register', data)
}




