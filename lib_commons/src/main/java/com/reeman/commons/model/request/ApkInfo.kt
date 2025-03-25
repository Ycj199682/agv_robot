package com.reeman.commons.model.request

import com.google.gson.annotations.SerializedName

data class ApkInfo(
    val name: String,
    val version: String,
    val changelog:String,
    val versionShort: String,
    val build:String,
    @SerializedName("updated_at")val updatedAt:Long,
    val installUrl:String,
    val binary: Binary,
    var localPath:String?,
) {
    override fun toString(): String {
        return "ApkInfo(name='$name', changeLog='$changelog', version='$version', versionShort='$versionShort', build='$build', updateAt=$updatedAt, installUrl='$installUrl', binary=$binary, localPath=$localPath)"
    }
}

data class Binary(val fsize:Long){
    override fun toString(): String {
        return "Binary(fsize=$fsize)"
    }
}
