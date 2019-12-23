import React from 'react';
import configTextUrl from './test_config_1.txt';

class Config extends React.Component {
	constructor(props) {
		super(props)
		this.state = {}
		fetchAsync(configTextUrl)
			.then(configText => this.setState({ configText }))
			.catch(e => console.error(e))
	}

	render() {
		return <div className="Config">
			<textarea className='Config-textarea'
				placeholder='Paste configuration here'
				value={this.state.configText || ''}
				onChange={e => this.setState({ configText: e.target.value })}
			/>
		</div>
	}
}

function fetchAsync(url) {
	return new Promise((resolve, reject) => {
		fetch(url).then(
			response => resolve(response.text()),
			reject)
	})
}

export default Config;
