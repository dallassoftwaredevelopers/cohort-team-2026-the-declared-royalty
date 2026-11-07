import { useCallback, useEffect, useState } from "react";

export type ResourceListState<T> =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "loaded"; items: T[] };

// Loads a list once, then lets the page show creates and updates without refetching
export function useResourceList<T>(load: () => Promise<T[]>, getId: (item: T) => string) {
  const [state, setState] = useState<ResourceListState<T>>({ status: "loading" });

  useEffect(() => {
    let active = true;

    load()
      .then((items) => {
        if (active) setState({ status: "loaded", items });
      })
      .catch((requestError: unknown) => {
        if (active) {
          setState({
            status: "error",
            message: requestError instanceof Error ? requestError.message : "Unable to load.",
          });
        }
      });

    return () => {
      active = false;
    };
  }, [load]);

  // Appends an item the backend just created
  const add = useCallback((item: T) => {
    setState((current) =>
      current.status === "loaded"
        ? { status: "loaded", items: [...current.items, item] }
        : { status: "loaded", items: [item] },
    );
  }, []);

  // Swaps in an item the backend just updated, keeping its place in the list
  const replace = useCallback(
    (updated: T) => {
      setState((current) =>
        current.status === "loaded"
          ? {
              status: "loaded",
              items: current.items.map((item) => (getId(item) === getId(updated) ? updated : item)),
            }
          : current,
      );
    },
    [getId],
  );

  // Drops an item the backend just deleted
  const remove = useCallback(
    (removed: T) => {
      setState((current) =>
        current.status === "loaded"
          ? { status: "loaded", items: current.items.filter((item) => getId(item) !== getId(removed)) }
          : current,
      );
    },
    [getId],
  );

  return { state, add, replace, remove };
}
