import axios from "axios";

const API = "http://localhost:8080/api/device";

// Register current device
export const registerDevice = async (device) => {
  const token = localStorage.getItem("token");

  return axios.post(`${API}/register`, device, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
};

// Get all registered devices
export const getDevices = async () => {
  const token = localStorage.getItem("token");

  return axios.get(`${API}/list`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
};

// Remove a device (for future use)
export const removeDevice = async (deviceId) => {
  const token = localStorage.getItem("token");

  return axios.delete(`${API}/${deviceId}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
};