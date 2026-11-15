import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import ClientePrestadores from "../pages/ClientePrestadores";
import Home from "../pages/Home";

export const Router: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/cliente/prestadores" element={<ClientePrestadores />} />
      </Routes>
    </BrowserRouter>
  );
};

export default Router;
