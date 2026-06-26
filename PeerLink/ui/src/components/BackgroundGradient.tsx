'use client';

import { ShaderGradientCanvas, ShaderGradient } from '@shadergradient/react';

export default function BackgroundGradient() {
  return (
    <div className="fixed inset-0 -z-10 bg-[#000]">
      <ShaderGradientCanvas
        style={{ position: 'absolute', inset: 0, width: '100vw', height: '100vh', pointerEvents: 'none' }}
        pixelDensity={1}
        fov={45}
      >
        <ShaderGradient
          animate="on"
          brightness={1.2}
          cAzimuthAngle={180}
          cDistance={3.6}
          cPolarAngle={90}
          cameraZoom={1}
          color1="#ff0026"
          color2="#6e0000"
          color3="#31cfe1"
          envPreset="city"
          lightType="3d"
          positionX={-1.4}
          positionY={0}
          positionZ={0}
          range="disabled"
          rangeEnd={40}
          rangeStart={0}
          reflection={0.1}
          rotationX={0}
          rotationY={10}
          rotationZ={50}
          shader="defaults"
          type="plane"
          uAmplitude={1}
          uDensity={1.3}
          uFrequency={5.5}
          uSpeed={0.4}
          uStrength={4}
          uTime={0}
          wireframe={false}
          grain="on"
        />
      </ShaderGradientCanvas>
    </div>
  );
}
