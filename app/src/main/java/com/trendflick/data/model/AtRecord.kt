package com.trendflick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AtRecord(
    @Json(name = "uri") val uri: String,
    @Json(name = "cid") val cid: String,
    @Json(name = "value") val value: Map<String, Any>
)

@JsonClass(generateAdapter = true)
data class ListRecordsResponse(
    @Json(name = "records") val records: List<AtRecord>,
    @Json(name = "cursor") val cursor: String?
)

@JsonClass(generateAdapter = true)
data class AtRecordRef(
    @Json(name = "uri") val uri: String,
    @Json(name = "cid") val cid: String
) 