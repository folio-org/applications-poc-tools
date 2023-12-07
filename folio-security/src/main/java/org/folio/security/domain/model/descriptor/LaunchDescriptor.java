package org.folio.security.domain.model.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class LaunchDescriptor {

  private String exec;
  private String cmdlineStart;
  private String cmdlineStop;
  private String dockerImage;
  private Boolean dockerPull;
  private Object dockerArgs;
  private Integer waitIterations;
  private List<EnvEntry> env;

  @JsonProperty("dockerCMD")
  private List<String> dockerCommand;

  public LaunchDescriptor exec(String exec) {
    this.exec = exec;
    return this;
  }

  public LaunchDescriptor cmdlineStart(String cmdlineStart) {
    this.cmdlineStart = cmdlineStart;
    return this;
  }

  public LaunchDescriptor cmdlineStop(String cmdlineStop) {
    this.cmdlineStop = cmdlineStop;
    return this;
  }

  public LaunchDescriptor dockerImage(String dockerImage) {
    this.dockerImage = dockerImage;
    return this;
  }

  public LaunchDescriptor dockerPull(Boolean dockerPull) {
    this.dockerPull = dockerPull;
    return this;
  }

  public LaunchDescriptor dockerCommand(List<String> dockerCommand) {
    this.dockerCommand = dockerCommand;
    return this;
  }

  public LaunchDescriptor addDockerCommandItem(String dockerCommandItem) {
    if (this.dockerCommand == null) {
      this.dockerCommand = new ArrayList<>();
    }
    this.dockerCommand.add(dockerCommandItem);
    return this;
  }

  public LaunchDescriptor dockerArgs(Object dockerArgs) {
    this.dockerArgs = dockerArgs;
    return this;
  }

  public LaunchDescriptor waitIterations(Integer waitIterations) {
    this.waitIterations = waitIterations;
    return this;
  }

  public LaunchDescriptor env(List<EnvEntry> env) {
    this.env = env;
    return this;
  }

  public LaunchDescriptor addEnvItem(EnvEntry envItem) {
    if (this.env == null) {
      this.env = new ArrayList<>();
    }
    this.env.add(envItem);
    return this;
  }
}
