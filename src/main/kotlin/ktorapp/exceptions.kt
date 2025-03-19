package ktorapp

/**
 * Thrown by ktor route handlers when we should display an error message to the user.
 */
data class BadUserInputException(override val message: String) : Exception()