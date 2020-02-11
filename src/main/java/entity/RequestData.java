package entity;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RequestData implements Serializable{
	
	private static final long serialVersionUID = 237733194593001583L;

	private String type;
	private String ip;
	private String parameters;
	private String payload;
	private Date date;

}
