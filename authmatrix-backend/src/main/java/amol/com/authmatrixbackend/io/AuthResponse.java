package amol.com.authmatrixbackend.io;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String email;
    private String jwtToken;
    private String name;
    private boolean isAccountVerified;
}
