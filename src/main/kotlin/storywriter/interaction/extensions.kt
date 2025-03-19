package storywriter.interaction

data class ResponseTriplet<out A : Response, out B : Response, out C : Response>(val a: A?, val b: B?, val c: C?) : Response {
	companion object {
		fun <A : Response, B : Response, C : Response> from(underlying: OneOfThree<A,B,C>): ResponseTriplet<A,B,C> {
			return when(underlying) {
				is OneOfThree.First<A> -> ResponseTriplet(underlying.value, null, null)
				is OneOfThree.Second<B> -> ResponseTriplet(null, underlying.value, null)
				is OneOfThree.Third<C> -> ResponseTriplet(null, null, underlying.value)
			}
		}
	}
}

sealed class OneOfThree<out A : Response, out B : Response, out C : Response> : Response {
	data class First<A : Response>(val value: A) : OneOfThree<A, Nothing, Nothing>()
	data class Second<B : Response>(val value: B) : OneOfThree<Nothing, B, Nothing>()
	data class Third<C : Response>(val value: C) : OneOfThree<Nothing, Nothing, C>()
}

suspend inline fun <
		reified A : Response,
		reified B : Response,
		reified C : Response,
> UserInteraction.pollForOneOfSealed(
	a: RequestComponent<A>,
	b: RequestComponent<B>,
	c: RequestComponent<C>? = null
): OneOfThree<A, B, C> {
	val response = this.pollForRaw("ONE_OF", listOfNotNull(a,b,c))
	val item = response.firstOrNull { it != null }
		?: throw IllegalStateException("got null for all results from pollForOneOfSealed")

	return when(item) {
		is A -> OneOfThree.First(item)
		is B -> OneOfThree.Second(item)
		is C -> OneOfThree.Third(item)
		else -> throw IllegalArgumentException("pollForOneOf got unhandled result: $response")
	}
}

suspend inline fun <
		reified A : Response,
		reified B : Response,
		reified C : Response,
> UserInteraction.pollForOneOf(
	a: RequestComponent<A>,
	b: RequestComponent<B>,
	c: RequestComponent<C>? = null
): ResponseTriplet<A, B, C> = ResponseTriplet.from(pollForOneOfSealed(a,b,c))

suspend inline fun <
		reified A : Response,
		reified B : Response,
> UserInteraction.pollForOneOf(
	a: RequestComponent<A>,
	b: RequestComponent<B>,
): ResponseTriplet<A, B, NoResponse> = ResponseTriplet.from(pollForOneOfSealed(a,b,null))

data class Quad<T1, T2, T3, T4>(
	val item1: T1?,
	val item2: T2?,
	val item3: T3?,
	val item4: T4?
)

suspend fun <R1 : Response, R2 : Response, R3 : Response, R4 : Response> UserInteraction.pollForMulti(
	component1: RequestComponent<R1>,
	component2: RequestComponent<R2>?,
	component3: RequestComponent<R3>?,
	component4: RequestComponent<R4>?
): Quad<R1, R2, R3, R4> {
	val request = listOfNotNull(component1, component2, component3, component4)
	val responses = pollForRaw("ALL_OF", request)

	assert(request.size == responses.size)

	@Suppress("UNCHECKED_CAST")
	return Quad(
		responses.getOrNull(0) as R1,
		responses.getOrNull(1) as? R2,
		responses.getOrNull(2) as? R3,
		responses.getOrNull(3) as? R4,
	)
}