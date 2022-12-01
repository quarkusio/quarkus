package com.example.quarkusmm.domain

import com.example.quarkusmm.port.CustomerService
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class CustomerServiceImpl : CustomerService {
    override fun getMessage(): String {
        return "howdy"
    }
}