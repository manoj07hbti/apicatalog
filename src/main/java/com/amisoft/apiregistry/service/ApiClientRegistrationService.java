package com.amisoft.apiregistry.service;


import com.amisoft.apiregistry.config.ApiCreateEventConfig;
import com.amisoft.apiregistry.entity.ClientRegistation;
import com.amisoft.apiregistry.model.ApiRegistryResponse;
import com.amisoft.apiregistry.model.ClientRegistrationRequest;
import com.amisoft.apiregistry.model.ClientRegistrationResponse;
import com.amisoft.apiregistry.model.EventApiClientRegistration;
import com.amisoft.apiregistry.repository.ClientRegistrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.amisoft.apiregistry.constant.ApiRegistryConstant.EXCHANGE_NAME;
import static com.amisoft.apiregistry.constant.ApiRegistryConstant.ROUTING_KEY;

@Service
@Slf4j
public class ApiClientRegistrationService {

    public static final String UNDER_SCORE = "-";
    public static final String EMPTY = "";
    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    ApplicationRegistrationService applicationRegistrationService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public Optional<ClientRegistrationResponse> registerClient(ClientRegistrationRequest clientRegistrationRequest){

        Optional<ApiRegistryResponse> apiRegistryResponse = applicationRegistrationService.
                                                   findApiByName(clientRegistrationRequest.getApplicationNameToRegister());

        if(apiRegistryResponse.isPresent()){

            ClientRegistation clientRegistation = clientRegistrationRepository
                    .findByClientApplicationNameAndApplicationNameToRegister(clientRegistrationRequest.getClientApplicationName(),
                            clientRegistrationRequest.getApplicationNameToRegister());

            if(null == clientRegistation) {

                log.info("Application found for name :" + clientRegistrationRequest.getApplicationNameToRegister());

                clientRegistation = new ClientRegistation();
                BeanUtils.copyProperties(clientRegistrationRequest, clientRegistation);
                clientRegistation.setRegistrationKey(UUID.randomUUID().toString().replace(UNDER_SCORE, EMPTY));

                log.info("Registering client " + clientRegistrationRequest.getClientApplicationName() +
                        " for application :" + clientRegistrationRequest.getApplicationNameToRegister());

                ClientRegistation clientRegistrationSaved = clientRegistrationRepository.save(clientRegistation);


                log.info("Registered client " + clientRegistrationRequest.getClientApplicationName() +
                        " for application :" + clientRegistrationRequest.getApplicationNameToRegister());

                ClientRegistrationResponse clientRegistrationResponse = new ClientRegistrationResponse();
                BeanUtils.copyProperties(clientRegistation, clientRegistrationResponse);

                rabbitTemplate.convertAndSend(EXCHANGE_NAME,ROUTING_KEY,new EventApiClientRegistration("client_registered"
                        ,clientRegistrationRequest.getApplicationNameToRegister()));

                return Optional.of(clientRegistrationResponse);
            }else{

                log.error("Client is already registered for the application ");
                return Optional.empty();

            }


        }else{

            log.error("Target application not found :"+clientRegistrationRequest.getApplicationNameToRegister());
            return Optional.empty();
        }


    }


    public Optional<List<ClientRegistrationResponse>> findAllRegisteredClient(){

        List<ClientRegistation> registeredClient =  clientRegistrationRepository.findAll();

        log.info("Total number of api found :"+registeredClient.size());
        List<ClientRegistrationResponse> clientRegistrationResponseList  = registeredClient.stream()
                .map(api -> convertToresponse(api)).collect(Collectors.toList());
        log.info("Converted to response");

        return Optional.of(clientRegistrationResponseList);
    }



    public Optional <List<ClientRegistrationResponse>> findRegisteredClientForAPI(String name){

        log.info("Searching clients for application  :"+name);

        List<ClientRegistation> clientRegistationList =  clientRegistrationRepository.findByApplicationNameToRegister(name);

        log.info("Total number of registered client for applicaation "+name +" is :"+clientRegistationList.size());
        List<ClientRegistrationResponse> clientRegistrationResponseList  = clientRegistationList.stream()
                .map(api -> convertToresponse(api)).collect(Collectors.toList());

        log.info("Converted to response:"+name);
        return Optional.of(clientRegistrationResponseList);
    }

    private ClientRegistrationResponse convertToresponse(ClientRegistation clientRegistation){

        ClientRegistrationResponse clientRegistrationResponse = new ClientRegistrationResponse();
        BeanUtils.copyProperties(clientRegistation,clientRegistrationResponse);
        return clientRegistrationResponse;
    }

}
