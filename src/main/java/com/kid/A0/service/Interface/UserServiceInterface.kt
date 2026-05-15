package com.kid.A0.service.Interface

import com.kid.A0.dto.UserRequest
import com.kid.A0.dto.UserResponse

interface UserServiceInterface {

    val users: List<UserResponse>

    fun getUser(id: Long): UserResponse

    fun createUser(userRequest: UserRequest): UserResponse

    fun deleteUser(id: Long): String

    fun updateUser(username: String, targetId: Long, userRequest: UserRequest): UserResponse

    fun getMe(username: String): UserResponse
}