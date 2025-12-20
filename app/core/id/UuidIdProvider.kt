package com.mahout.app.core.id

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UuidIdProvider @Inject constructor() : IdProvider {
    override fun newId(): String = UUID.randomUUID().toString()
}
