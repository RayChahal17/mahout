package com.mahout.app.domain.path.model

/**
 * TIME actions are tracked via Sessions.
 * CHECK actions are tracked via CheckEvents.
 */
enum class ActionTrackingType {
    TIME,
    CHECK
}
