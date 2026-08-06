import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { getCurrentUser } from "../lib/api";

const UserContext = createContext(null);

export function UserProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchCurrentUser = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const user = await getCurrentUser();
      setCurrentUser(user);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch on mount
    fetchCurrentUser();
  }, [fetchCurrentUser]);

  return (
    <UserContext.Provider value={{ currentUser, loading, error }}>
      {children}
    </UserContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components -- hook is tightly coupled to UserProvider
export function useCurrentUser() {
  return useContext(UserContext);
}
