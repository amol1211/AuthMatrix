import { useContext } from "react";
import header from "../assets/85e5492d-f360-4273-92b0-4ce2324807cb.jpg";
import { AppContext } from "../context/AppContext";

const Header = () => {
  const { userData } = useContext(AppContext);
  return (
    <div
      className="text-center d-flex flex-column align-items-center justify-content-center.py-5 px-3"
      style={{ minHeight: "80vh" }}
    >
      <img src={header} alt="Header" width={120} className="mb-4" />

      <h5 className="fw-semibold" style={{ color: "#0d6efd" }}>
        Hey {userData ? userData.name : "Developer"}
        <span role="img" aria-label="wave">
          👋
        </span>
      </h5>
      <h1 className="fw-bold display-5 mb-3" style={{ color: "#00008b" }}>
        Welcome to AuthMatrix
      </h1>

      <p
        className=" fs-5 mb-4"
        style={{
          maxWidth: "500px",
          color: "#0d6efd",
          fontWeight: "normal",
          textAlign: "center",
        }}
      >
        Your one-stop solution for managing authentication and authorization
        with ease.
      </p>

      <button className="btn btn-outline-dark rounded-pill.px-4.py-2">
        Let's get started!
      </button>
    </div>
  );
};

export default Header;
