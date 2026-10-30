import { useCallback, useEffect, useState } from "react";
import api, { errorMessage } from "../api/api";

/** Loads data from the API and keeps it in state. */
export default function useFetch(path) {
  const [state, setState] = useState({ data: null, loading: true, error: null });

  const reload = useCallback(async () => {
    setState((s) => ({ ...s, loading: true }));
    try {
      const res = await api.get(path);
      setState({ data: res.data, loading: false, error: null });
    } catch (err) {
      setState({ data: null, loading: false, error: errorMessage(err, "Could not load data") });
    }
  }, [path]);

  useEffect(() => {
    reload();
  }, [reload]);

  const setData = useCallback(
    (update) =>
      setState((s) => ({
        ...s,
        data: typeof update === "function" ? update(s.data) : update,
      })),
    []
  );

  return { ...state, reload, setData };
}
