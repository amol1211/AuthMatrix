import Header from "../components/header";
import Menubar from "../components/Menubar";

const Home = () => {
  return (
    <div className="flex flex-col items-center justify-content-center min-vh-100">
      <Menubar />
      <Header />
    </div>
  );
};

export default Home;
