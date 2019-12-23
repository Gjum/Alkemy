import React from 'react';

const flaskPath = 'M120,20 v20 L180,170 a10,10 0 0,1 -10,10 h-140 a10,10 0 0,1 -10,-10 L80,40 v-20'

export const flaskClipPath = <clipPath id="flaskClip"><path d={flaskPath} /></clipPath>

export class Flask extends React.Component {
	constructor(props) {
		super(props)
		this.state = {}
	}

	render() {
		const { name, maxVolume, liquidVolume } = this.props.container
		const { liquidIn, gasIn, gasOut } = this.props.wiring[name] || {}
		const fillLevel = liquidVolume / maxVolume
		return <svg className="container-vis-svg flask-vis" viewBox='0 0 200 200' >
			{flaskClipPath}
			<rect className='container-vis-fill'
				x={20} y={180 - 160 * fillLevel} width={160} height={200}
				fill={this.props.color || 'aqua'} fillOpacity={.5} clipPath="url(#flaskClip)"
			/>
			<path className='container-vis-stroke'
				strokeWidth={4} stroke='black' strokeLinecap='round' strokeLinejoin='round' fill='none'
				d={flaskPath}
			/>

			{!liquidIn ? null : <path className='container-vis-liquidIn-pipe'
				strokeWidth={6} stroke='darkblue' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M5,0 v2 a10,10 0 0,0 10,10 H78 a10,10 0 0,1 10,10 V170'
			/>}
			{!liquidIn ? null : <path className='container-vis-liquidIn-fill'
				strokeWidth={2} stroke='aqua' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M5,0 v2 a10,10 0 0,0 10,10 H78 a10,10 0 0,1 10,10 V170'
			/>}

			{!gasIn ? null : <path className='container-vis-gasIn-pipe'
				strokeWidth={6} stroke='darkgreen' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M195,0 v2 a10,10 0 0,1 -10,10 H122 a10,10 0 0,0 -10,10 V170'
			/>}
			{!gasIn ? null : <path className='container-vis-gasIn-fill'
				strokeWidth={2} stroke='white' strokeLinecap='butt' strokeLinejoin='round' fill='none'
				d='M195,0 v2 a10,10 0 0,1 -10,10 H122 a10,10 0 0,0 -10,10 V170'
			/>}
		</svg>
	}
}
