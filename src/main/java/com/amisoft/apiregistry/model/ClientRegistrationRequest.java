package com.amisoft.apiregistry.model;


import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ClientRegistrationRequest {

    @NonNull
    private String clientApplicationName;
    @NonNull
    private String clientApplicationOwner;
    @NonNull
    private String clientApplicationOwnerEmail;
    @NonNull
    private String applicationNameToRegister;


}
