{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    devshell.url = "github:numtide/devshell";
    devshell.inputs.nixpkgs.follows = "nixpkgs";
  };

  outputs = inputs@{ flake-parts, self, ... }:
  flake-parts.lib.mkFlake { inherit inputs; } {
    imports = [
      inputs.devshell.flakeModule
    ];
    systems = [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" "x86_64-darwin" ];
    perSystem = { config, pkgs, system, ... }: {
      _module.args.pkgs = import self.inputs.nixpkgs {
        inherit system;
        config.allowUnfree = true;
      };
      devshells.default = {
        packages = with pkgs; [
          clojure
          (vscode-with-extensions.override {
            vscodeExtensions = with vscode-extensions; [
              betterthantomorrow.calva
            ];
          })
        ];
      };
    };
  };
}
