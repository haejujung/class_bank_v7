package com.tenco.bank.repository.model;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class User {
	private Integer id;
	private String username;
	private String password;
	private String fullname;
	private String originFileName;
	private String uploadFileName;
	private Timestamp createdAt;
	
	public String setUpUserImage() {
        System.out.println("파일 이름 : " + uploadFileName);
        String image = null;

        if(originFileName != null && uploadFileName == null) {
            image = originFileName;
        } else {
            image = uploadFileName == null ? 
                    "https://picsum.photos/id/1/350" : "/images/uploads/" + uploadFileName;
        }
        return image;
    }
}