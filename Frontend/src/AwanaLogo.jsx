import React from 'react';
import awanaLogoImg from './assets/awana-1-logo-png-transparent.png';

const AwanaLogo = ({ width = "150px" }) => (
    <img
        src={awanaLogoImg}
        alt="Awana Logo"
        style={{ width: width, height: 'auto', display: 'block', margin: '0 auto' }}
    />
);
export default AwanaLogo;