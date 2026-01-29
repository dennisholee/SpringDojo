package io.forest.security.security.userdetails;

import lombok.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@EqualsAndHashCode
@ToString
public class AppUserDetails implements UserDetails {

    private Collection<SimpleGrantedAuthority> authorities;

    private String username;

    private String password;

    public static class Builder {

        final String username;
        final String password;
        final Collection<SimpleGrantedAuthority> authorities = new Vector<>();


        public Builder(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public void addAuthorities(String resource, String permission) {
            authorities.add(new SimpleGrantedAuthority("%S_%S".formatted(resource, permission)));
        }

        public AppUserDetails build() {
            AppUserDetails appUserDetails = new AppUserDetails();
            appUserDetails.username = this.username;
            appUserDetails.password = this.password;
            appUserDetails.authorities = Collections.unmodifiableCollection(this.authorities);
            return appUserDetails;
        }
    }
}
