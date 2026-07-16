package org.glud.credentials.auth.service;

import lombok.RequiredArgsConstructor;
import org.glud.credentials.auth.repository.UserRepository;
import org.glud.credentials.auth.utilities.DtoUtility;
import org.glud.credentials.auth.utilities.RequestDTO;
import org.springframework.stereotype.Service;
import org.glud.credentials.security.components.JwtUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public String createResource(RequestDTO dto) {
        userRepository.findByUsername(DtoUtility.DtoConverter(dto).getUsername());
        //Buscar al usuario en la base de datos por ID
        //Si lo encuentra (implementar verificación de contraseñas hasheadas en la base de datos - bcrypt)
        //Si sale bien, llama a JwtUtils para crear el token con UserId, tenantId y roleId
        // Devuelve el token
        return "";
    }
}
