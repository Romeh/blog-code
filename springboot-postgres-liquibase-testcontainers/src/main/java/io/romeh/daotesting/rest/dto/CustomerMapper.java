package io.romeh.daotesting.rest.dto;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import io.romeh.daotesting.domain.Customer;

/**
 * @author romeh
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {

	CustomerDto mapCustomerToDto(Customer customer);

	Customer mapDtoToCustomer(CustomerDto customerDto);
}
