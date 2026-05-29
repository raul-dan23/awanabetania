import React from 'react';
import awanaLogoImg from './assets/awana-1-logo-png-transparent.png';

/**
 * Renders the Awana club logo image at a configurable width.
 *
 * @param {Object} props
 * @param {string} [props.width="150px"] - CSS width value passed directly to the img element
 */
const AwanaLogo = ({ width = "150px" }) => (
    <img
        src={awanaLogoImg}
        alt="Awana Logo"
        style={{ width: width, height: 'auto', display: 'block', margin: '0 auto' }}
    />
);

export default AwanaLogo;
