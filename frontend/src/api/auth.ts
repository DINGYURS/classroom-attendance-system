import request from '@/utils/request'
import type { 
  UserLoginDTO, 
  UserRegisterDTO,
} from '@/types/api'

// 用户登录
export function login(data: UserLoginDTO) {
  return request.post('/user/login', data)
}

// 用户注册
export function register(data: UserRegisterDTO) {
  return request.post('/user/register', data)
}




