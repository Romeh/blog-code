package io.romeh.daotesting.rest.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ApiModel(description = "All details about the customer. ")
public class CustomerDto {
	@NotNull
	@NotEmpty
	private String name;
	@NotNull
	@NotEmpty
	private String address;
	private boolean is_active;
}
