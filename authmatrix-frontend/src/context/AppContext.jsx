import axios from "axios";
import { createContext, useEffect, useState } from "react";
import { AppConstants } from "../util/constants.js";
import { toast } from "react-toastify";
export const AppContext = createContext();

export const AppContextProvider = (props) => {
  axios.defaults.withCredentials = true; // Enable cookies for cross-origin requests

  const backendURL = AppConstants.BACKEND_URL;
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userData, setUserData] = useState(false);

  /* const getUserData = async () => {
    try {
      const response = await axios.get(backendURL + "/profile");
      if (response.status === 200) {
        setUserData(response.data);
      } else {
        toast.error("Failed to fetch profile ");
      }
    } catch (error) {
      toast.error(error.message);
    }
  }; */

  const getAuthState = async () => {
    try {
      const response = await axios.get(backendURL + "/is-authenticated");
      if (response.status === 200 && response.data === true) {
        setIsLoggedIn(true);
        //await getUserData();
      } else {
        setIsLoggedIn(false);
        setUserData(null); //new add
      }
    } catch (error) {
      //console.error(error);
      // This catch block will be hit for network errors or non-2xx responses (like 401)
      if (error.response?.status === 401) {
        // If the backend responds with 401, it means the user is not authenticated.
        // This is expected behavior when no valid session cookie is sent or found.
        setIsLoggedIn(false);
        setUserData(null); // Clear user data as they are not logged in
        // No need to toast an error for expected 401 on initial load
      } else {
        // Handle other unexpected errors
        console.error("Error checking authentication state:", error);
        toast.error(
          error.response?.data?.message ||
            error.message ||
            "An unexpected error occurred."
        );
      }
    }
  };

  useEffect(() => {
    getAuthState();
  }, []);

  const contextValue = {
    backendURL,
    isLoggedIn,
    setIsLoggedIn,
    userData,
    setUserData,
    //getUserData,
  };

  return (
    <AppContext.Provider value={contextValue}>
      {props.children}
    </AppContext.Provider>
  );
};
