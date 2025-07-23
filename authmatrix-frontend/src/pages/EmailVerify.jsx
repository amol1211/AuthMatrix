import { Link, useNavigate } from "react-router-dom";
import logo from "../assets/logo.png";
import { useContext, useEffect, useRef, useState } from "react";
import { AppContext } from "../context/AppContext";
import axios from "axios";
import { toast } from "react-toastify";

const EmailVerify = () => {
  const inputRef = useRef([]);
  const [loading, setLoading] = useState(false);
  const { getUserData, isLoggedIn, userData, backendURL } =
    useContext(AppContext);
  const navigate = useNavigate();

  const handleChange = (e, index) => {
    const value = e.target.value.replace(/\D/, "");
    e.target.value = value;
    if (value && index < 5) {
      inputRef.current[index + 1].focus();
    }
  };

  const handleKeyDown = (e, index) => {
    if (e.key === "Backspace" && index > 0 && !e.target.value) {
      inputRef.current[index - 1].focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasteData = e.clipboardData.getData("text").slice(0, 6).split("");
    pasteData.forEach((digit, index) => {
      if (inputRef.current[index]) {
        inputRef.current[index].value = digit;
      }
    });

    const next = pasteData.length < 6 ? pasteData.length : 5;
    inputRef.current[next].focus();
  };

  const handleVerify = async () => {
    const otp = inputRef.current.map((input) => input.value).join("");
    if (otp.length < 6) {
      toast.error("Please enter a valid OTP");
      return;
    }

    setLoading(true);
    try {
      const response = await axios.post(backendURL + "/api/verify-otp", {
        otp,
      });
      if (response.status === 200) {
        toast.success("Email verified successfully!");
        await getUserData();
        navigate("/");
      } else {
        toast.error("Invalid OTP, please try again.");
      }
    } catch (error) {
      toast.error("Failed to verify OTP. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    isLoggedIn && userData && userData.isAccountVerified && navigate("/");
  }, [isLoggedIn, userData]);

  return (
    <div
      className="email-verify-container d-flex align-items-center justify-content-center vh-100 position-relative"
      style={{
        background: "#ffffff",
        border: "none",
      }}
    >
      <Link
        to="/"
        className="position-absolute top-0 start-0 p-4 d-flex align-items-center gap-2 text-decoration-none"
      >
        <img src={logo} alt="Logo" height={50} width={200} />
        <span className="fs-4 fw-semibold text-light"></span>
      </Link>

      <div className="p-5 rounded-4 shadow bg-white" style={{ width: "400px" }}>
        <h4 className="text-center fw-bold mb-2">Email Verify OTP</h4>
        <p className="text-center mb-4">
          Enter the OTP sent to your email to verify your account.
        </p>
        <div className="d-flex justify-content-between gap-2 mb-4 text-center text-white-50 mb-2">
          {[...Array(6)].map((_, index) => (
            <input
              key={index}
              type="text"
              maxLength={1}
              className="form-control text-center fs-4 otp-input"
              ref={(el) => (inputRef.current[index] = el)}
              onChange={(e) => handleChange(e, index)}
              onKeyDown={(e) => handleKeyDown(e, index)}
              onPaste={handlePaste}
            />
          ))}
        </div>
        <button
          className="btn btn-primary w-100 fw-semibold"
          disabled={loading}
          onClick={handleVerify}
        >
          {loading ? "Verifying..." : "Verify email"}
        </button>
      </div>
    </div>
  );
};
export default EmailVerify;
