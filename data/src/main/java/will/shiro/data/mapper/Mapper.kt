package will.shiro.data.mapper

/**
 * The abstract mapper that every ApiEntity must implement so they can be converted to one usable in the application
 * */
abstract class Mapper<in I, out O> {
    abstract fun transform(t: I): O
    fun transformList(items: List<I>?): List<O>? = items?.map(::transform)
}