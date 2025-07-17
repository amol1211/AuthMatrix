import { useNavigate } from "react-router-dom";
import logo from "../assets/logo-home.png";
import { useContext, useRef, useState } from "react";
import { AppContext } from "../context/AppContext";

const Menubar = () => {
  const navigate = useNavigate();
  const { userData } = useContext(AppContext);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);
  return (
    <nav className="navbar bg-white px-5 py-4 d-flex justify-content-between align-items-center">
      <div className="d-flex align-items-center gap-2">
        <img src={logo} alt="Logo" width={200} height={200} />
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
                >
                  verify your email
                </div>
              )}
              <div
                className="dropdown-tem py-1 px-2 text-danger"
                style={{ cursor: "pointer" }}
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
