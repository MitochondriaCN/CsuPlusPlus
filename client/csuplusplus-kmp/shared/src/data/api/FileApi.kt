package ink.qingshuige.csuplusplus.data.api

import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.data.dto.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * 文件与课程资料接口，对应后端 `FileController`。
 */
class FileApi(private val api: ApiClient) {

    suspend fun getMetadata(id: Long): Result<FileMetadataResponse> =
        api.get("file") {
            parameter("id", id)
        }

    suspend fun listCourseFiles(
        courseId: Long,
        query: PageQuery = PageQuery(),
    ): Result<PagedResponse<FileMetadataResponse>> =
        api.get("file/list/course") {
            parameter("courseId", courseId)
            pageParams(query)
        }

    suspend fun searchCourseFiles(
        courseId: Long,
        keyword: String,
        query: PageQuery = PageQuery(),
    ): Result<PagedResponse<FileMetadataResponse>> =
        api.get("file/search/course") {
            parameter("courseId", courseId)
            parameter("keyword", keyword)
            pageParams(query)
        }

    suspend fun uploadCourseFile(
        courseId: Long,
        fileName: String,
        fileBytes: ByteArray,
        friendlyFilename: String? = null,
        contentType: String? = null,
    ): Result<FileMetadataResponse> =
        api.postMultipart(
            path = "file/upload/course",
            queryAndHeaders = { parameter("courseId", courseId) },
        ) {
            val escapedFileName = fileName.replace("\"", "\\\"")
            append(
                key = "file",
                value = fileBytes,
                headers = Headers.build {
                    append(
                        HttpHeaders.ContentDisposition,
                        "form-data; name=\"file\"; filename=\"$escapedFileName\"",
                    )
                    contentType?.let { append(HttpHeaders.ContentType, it) }
                },
            )
            friendlyFilename?.let { append("friendlyFilename", it) }
        }

    suspend fun bindCourseFile(courseId: Long, fileId: Long): Result<CourseFileResponse> =
        api.post("file/bind/course") {
            parameter("courseId", courseId)
            parameter("fileId", fileId)
        }

    suspend fun downloadCourseFile(id: Long): Result<DownloadedFile> =
        api.downloadFile("file/download/course/$id")
}
