package edu.dosw.restaurante.service;

import edu.dosw.restaurante.model.domain.Usuario;
import edu.dosw.restaurante.security.Sesion;

public interface IAuthService {
    Sesion login(String email, String password);
    Usuario registrarCliente(String nombre, String email, String password);
    Usuario obtenerPorEmail(String email);
}
