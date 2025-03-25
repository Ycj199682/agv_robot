package com.reeman.commons.exceptions

/**
 * code:
 *      -1:no response body
 *      -2:request failed
 *      -3:redirect too much
 *      -4:not found file name
 *      10021: file type warn
 *      10010: file not exist
 */
class CustomHttpException(val code:Int, override val message:String):Exception(message)