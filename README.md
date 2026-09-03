# VulkanMod for Android

<img src="assets/vulkanmod/vulkan-mod_android.png" width=200 height="200">

**Credits:xCollateral**

This repository contains a Vulkan-based renderer mod for Minecraft: Java Edition, ported for Android devices. This mod aims to significantly improve performance and graphical fidelity by leveraging the modern Vulkan graphics API. It is designed to be used with popular custom launchers such as FoldCraft Launcher, Zalith Launcher, and TurtleLauncher.
---
## Features
 * Enhanced Performance: By replacing the OpenGL renderer with the more efficient Vulkan API, this mod can lead to higher frame rates and smoother gameplay, especially on modern hardware.
 * Reduced CPU Overhead: Vulkan's architecture allows for better distribution of rendering tasks, reducing the CPU bottleneck and freeing up resources for other processes.
 * Improved Graphical Capabilities: Paves the way for advanced graphical features and optimizations that are not possible with the older OpenGL API.
 * Optimized for Android: Specifically tailored to run on Android devices, taking advantage of the prevalent support for Vulkan in recent Android versions.
Compatibility
 * Android version: 10+
 * Operating System: Android
 * Vulkan Support required: Yes
 * Minium vulkan version: Vulkan 1.2
 * Launchers:
   * Zalith Launcher
   * PojavLauncher
   * FoldCraft Launcher
   * Turtle launcher
   * Cryonix launcher
   * HyperX launcher
   * RX launcher
   * MJ launcher/mojo launcher
   * SolCraft launcher
   * Copper launcher
 * Mod Incompatibility: This mod is fundamentally incompatible with most mods that directly interact with or modify the OpenGL rendering pipeline. This includes many popular optimization and shader mods.
Installation
Prerequisites
 * An Android device with an ARM64 processor.
 * One of the supported launchers installed.
 * The Fabric mod loader installed for your desired Minecraft version within the launcher.
General Instructions
 * Download the Mod: Grab the latest release of the VulkanMod for ARM64 from the [Releases](https://github.com/Endiq-jar/VulkanMod-Android/releases) page.
* Place the downloaded .jar file into the mods folder.
Launcher-Specific Instructions
Building from Source (For Developers)
To compile this mod from its source code, you will need:
 * Java Development Kit (JDK) 8 or newer.
 * Git.
<!-- end list -->
# Clone the repository
```
git clone [https://github.com/shindozk/VulkanMod-Android-ARM64]
cd [repository-folder]
```

# Build the project (example using Gradle)
`./gradlew build`

The compiled .jar file will be located in the build/libs directory.
Contributing
Contributions from the community are welcome! If you would like to contribute, please fork the repository and submit a pull request with your changes. Before contributing, please open an issue to discuss the proposed changes.
License
This project is licensed under the MIT License.
