// Error body returned by the backend's GlobalExceptionHandler (e.g. vendor 404s and 400s)
export type ErrorResponse = {
    status: number,
    error: string,
    message: string,
    path?: string,
    timestamp: string,
}
