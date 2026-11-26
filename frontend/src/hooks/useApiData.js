import { useCallback, useEffect, useState } from 'react'
import { toApiError } from '../api/errors.js'

/**
 * Loads data when the component mounts and whenever `deps` change.
 * Returns { data, error, loading, reload }.
 *
 * `loader` returns a Promise, or null to skip loading (e.g. while a form value is invalid).
 */
export function useApiData(loader, deps) {
  const [state, setState] = useState({ data: null, error: null, loading: true })
  const [reloadCount, setReloadCount] = useState(0)

  useEffect(() => {
    const promise = loader()
    if (!promise) {
      setState({ data: null, error: null, loading: false })
      return undefined
    }

    // ignore late answers: if the deps changed meanwhile, an older response must not overwrite a newer one
    let stale = false
    setState((previous) => ({ ...previous, loading: true, error: null }))
    promise
      .then((data) => !stale && setState({ data, error: null, loading: false }))
      .catch((error) => !stale && setState({ data: null, error: toApiError(error), loading: false }))

    return () => {
      stale = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- callers pass the deps that matter
  }, [...deps, reloadCount])

  const reload = useCallback(() => setReloadCount((count) => count + 1), [])
  return { ...state, reload }
}
