import React from 'react';

export const canisterClipPath = <clipPath id="canisterClip">
	<rect x={20} y={20} width={160} height={160} rx={10} />
</clipPath>

export class Canister extends React.Component {
	constructor(props) {
		super(props)
		this.state = {}
	}

	render() {
		const { name, maxVolume, liquidVolume } = this.props.container
		const { liquidIn, gasIn, liquidOut, gasOut } = this.props.wiring[name] || {}
		const fillLevel = liquidVolume / maxVolume
		return <svg className="container-vis-svg canister-vis" viewBox='0 0 200 200'>
			{canisterClipPath}
			<rect className='container-vis-fill'
				x={20} y={180 - 160 * fillLevel} width={160} height={200}
				fill={this.props.color || 'aqua'} fillOpacity={.5} clipPath="url(#canisterClip)"
			/>
			<rect className='container-vis-stroke'
				x={20} y={20} width={160} height={160} rx={10}
				strokeWidth={4} stroke='black' fill='none'
			/>

			{!liquidIn ? null : <path className='container-vis-liquidIn-pipe'
				strokeWidth={6} stroke='darkblue' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M5,0 V30 a10,10 0 0,0 10,10 H22'
			/>}
			{!liquidIn ? null : <path className='container-vis-liquidIn-fill'
				strokeWidth={2} stroke='aqua' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M5,0 V30 a10,10 0 0,0 10,10 H22'
			/>}

			{!gasIn ? null : <path className='container-vis-gasIn-pipe'
				strokeWidth={6} stroke='darkgreen' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M195,0 a10,10 0 0,1 -10,10 H50 a10,10 0 0,0 -10,10 V22'
			/>}
			{!gasIn ? null : <path className='container-vis-gasIn-fill'
				strokeWidth={2} stroke='white' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M195,0 a10,10 0 0,1 -10,10 H50 a10,10 0 0,0 -10,10 V22'
			/>}

			{!liquidOut ? null : <path className='container-vis-liquidOut-pipe'
				strokeWidth={6} stroke='darkblue' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M5,200 V180 a10,10 0 0,1 10,-10 H22'
			/>}
			{!liquidOut ? null : <path className='container-vis-liquidOut-fill'
				strokeWidth={2} stroke='aqua' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M5,200 V180 a10,10 0 0,1 10,-10 H22'
			/>}

			{!gasOut ? null : <path className='container-vis-gasOut-pipe'
				strokeWidth={6} stroke='darkgreen' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M195,200 V40 a10,10 0 0,0 -10,-10 H178'
			/>}
			{!gasOut ? null : <path className='container-vis-gasOut-fill'
				strokeWidth={2} stroke='white' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M195,200 V40 a10,10 0 0,0 -10,-10 H178'
			/>}
		</svg>
	}
}
