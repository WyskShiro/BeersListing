package will.shiro.data.api

import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import will.shiro.data.BuildConfig
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleTransformer
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import will.shiro.data.entity.ApiBeer
import will.shiro.data.request.RequestException

/**
 * The bridge between ApiService and DefaultRepositories
 * */
object ApiClient {

    private const val apiURL = BuildConfig.API_URL
    private lateinit var retrofit: Retrofit
    private var apiServiceSingleton: ApiService? = null

    private val apiServices: ApiService get() = apiServiceSingleton ?: buildApiServices()

    /**
     * Beers
     * */

    fun getBeers(): Single<List<ApiBeer>> {
        return makeRequest(apiServices.getBeers())
    }

    /**
     *
     * - ApiService, Retrofit,
     * - Response and Request Handler Methods
     *
     **/

    private fun buildApiServices(): ApiService {
        retrofit = Retrofit.Builder()
            .baseUrl(apiURL)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .serializeNulls()
                        .create()
                )
            )
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        with(retrofit.create(ApiService::class.java)) {
            apiServiceSingleton = this
            return this
        }
    }

    private fun <T> verifyResponseException(): SingleTransformer<Response<T>, Response<T>> {
        return SingleTransformer { upstream ->
            upstream.doOnSuccess { response ->
                if (!response.isSuccessful) {
                    throw Throwable("Something went wrong while getting the response")
                }
            }
        }
    }

    private fun <T> verifyRequestException(): SingleTransformer<Response<T>, Response<T>> {
        return SingleTransformer { upstream ->
            upstream.onErrorResumeNext { t ->
                when {
                    else -> Single.error(RequestException.unexpectedError(t))
                }
            }
        }
    }

    private fun <T> unwrap(): SingleTransformer<Response<T>, T> {
        return SingleTransformer { upstream ->
            upstream.map<T> { it.body()!! }
        }
    }

    private fun <T> makeRequest(request: Single<Response<T>>): Single<T> {
        return request.compose(verifyResponseException())
            .compose(verifyRequestException())
            .compose(unwrap())
    }

    private fun <T> justVerifyErrors(request: Single<Response<T>>): Completable {
        return request.compose(verifyResponseException())
            .compose(verifyRequestException())
            .ignoreElement()
    }
}
