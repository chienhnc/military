package com.military.payload.response;

public class BaseResponse<T> {
  private int httpStatus;
  private T data;
  private String path;

  public BaseResponse(int httpStatus, T data, String path) {
    this.httpStatus = httpStatus;
    this.data = data;
    this.path = path;
  }

  public static <T> BaseResponse<T> of(int httpStatus, T data, String path) {
    return new BaseResponse<>(httpStatus, data, path);
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus(int httpStatus) {
    this.httpStatus = httpStatus;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
