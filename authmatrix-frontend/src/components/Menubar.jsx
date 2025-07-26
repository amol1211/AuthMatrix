import { useNavigate } from "react-router-dom";
import logo from "../assets/logo.png";
import { useContext, useEffect, useRef, useState } from "react";
import { AppContext } from "../context/AppContext";
import axios from "axios";
import { toast } from "react-toastify";

const Menubar = () => {
  const navigate = useNavigate();
  const { userData, backendURL, setUserData, setIsLoggedIn } =
    useContext(AppContext);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const handleLogout = async () => {
    try {
      axios.defaults.withCredentials = true;
      const response = await axios.post(backendURL + "/logout");
      if (response.status === 200) {
        setIsLoggedIn(false);
        setUserData(null);
        navigate("/login");
      }
    } catch (error) {
      console.error("Logout error:", error);
      toast.error(
        error.response?.data?.message || "Failed to log out. Please try again."
      );
    }
  };

  const sendVerificationOtp = async () => {
    try {
      console.log("Sending OTP request to:", backendURL + "/send-otp");
      console.log("User data:", userData);

      // Set axios defaults
      axios.defaults.withCredentials = true;

      const response = await axios.post(
        backendURL + "/send-otp",
        {}, // Empty body since we're getting email from JWT
        {
          withCredentials: true,
          headers: {
            "Content-Type": "application/json",
          },
        }
      );

      console.log("OTP Response:", response);

      if (response.status === 200) {
        navigate("/email-verify");
        toast.success("OTP has been sent successfully!");
      } else {
        toast.error("Failed to send OTP. Please try again!");
      }
    } catch (error) {
      console.error("OTP Error Details:", {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status,
        headers: error.response?.headers,
      });

      if (error.response?.status === 401) {
        toast.error("You need to log in again. Session expired.");
        setIsLoggedIn(false);
        setUserData(null);
        navigate("/login");
      } else if (error.response?.status === 403) {
        toast.error("Access denied. Please log in again.");
        setIsLoggedIn(false);
        setUserData(null);
        navigate("/login");
      } else {
        toast.error(
          error.response?.data?.message ||
            error.response?.data ||
            "Error sending OTP. Please try again."
        );
      }
    }
  };

  return (
    <nav className="navbar bg-white px-5 py-4 d-flex justify-content-between align-items-center">
      <div className="d-flex align-items-center gap-2">
        <img src={logo} alt="Logo" width={200} height={50} />
        <span className="fw-bold fs-4 text-dark"></span>
      </div>

      {userData ? (
        <div className="position-relative" ref={dropdownRef}>
          <div
            className="bg-dark text-white rounded-circle d-flex justify-content-center align-items-center"
            style={{
              width: "40px",
              height: "40px",
              cursor: "pointer",
              userSelect: "none",
            }}
            onClick={() => setDropdownOpen((prev) => !prev)}
          >
            {userData.name[0].toUpperCase()}
          </div>
          {dropdownOpen && (
            <div
              className="position-absolute shadow bg-white  rounded p-2"
              style={{ top: "50px", right: "0", zIndex: 100 }}
            >
              {!userData.isAccountVerified && (
                <div
                  className="dropdown-item py-1 px-2"
                  style={{ cursor: "pointer" }}
                  onClick={sendVerificationOtp}
                >
                  verify your email
                </div>
              )}
              <div
                className="dropdown-item py-1 px-2 text-danger"
                style={{ cursor: "pointer" }}
                onClick={handleLogout}
              >
                Logout
              </div>
            </div>
          )}
        </div>
      ) : (
        <div
          className="btn btn-outline-dark rounded-pill px-3"
          onClick={() => navigate("/login")}
        >
          Login <i className="bi bi-arrow-in-right ms-2"></i>
        </div>
      )}
    </nav>
  );
};

export default Menubar;
// This code defines a Menubar component that displays a navigation bar with a logo, user profile options, and a login button. It handles user authentication, logout, and email verification functionalities using React hooks and Axios for API requests. The component also includes a dropdown menu for user actions and manages click events outside the dropdown to close it.
