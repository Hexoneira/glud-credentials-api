package org.glud.credentials.auth.utilities;

import org.glud.credentials.auth.model.User;

public class DtoUtility {

    public static User DtoConverter(RequestDTO requestDTO) {
        User user = new User();
        user.setUsername(requestDTO.username());
        user.setPassword(requestDTO.password());
        return user;
    }
}
