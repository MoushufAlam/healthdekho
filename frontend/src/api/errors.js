// Turns any Axios error into { status, message, fieldErrors }.
// The backend always answers errors with the ApiError JSON shape (Step 10),
// so components only ever deal with this one format.
export function toApiError(error) {
  const body = error?.response?.data

  if (body?.message) {
    return {
      status: body.status ?? error.response.status,
      message: body.message,
      fieldErrors: body.fieldErrors ?? {},
    }
  }
  if (error?.code === 'ECONNABORTED') {
    return { status: 0, message: 'The server took too long to respond. Please try again.', fieldErrors: {} }
  }
  if (!error?.response) {
    return {
      status: 0,
      message: 'Cannot reach the server. Make sure the backend is running on port 8080.',
      fieldErrors: {},
    }
  }
  return { status: error.response.status, message: 'Something went wrong. Please try again.', fieldErrors: {} }
}
